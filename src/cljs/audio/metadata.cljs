(ns audio.metadata
  (:require [cljs.nodejs :as nodejs]
            [audio.constants :as cs]
            [audio.types :as atypes :refer [Track]]
            [filesystem.path :refer [get-extension]]
            [reagent.core :as reagent]
            [filesystem.application :as app-fs]
            [filesystem.io :as fio]
            [sonority.util :refer [map-vals map-keys]]))


(defonce md (nodejs/require "audio-metadata"))

(defonce fetch-manager (atom {:running 0 :queue []}))
(defonce fetches-max 8)

(nodejs/enable-util-print!)

(defonce read-config
  (comp
    #(map-vals %
      (fn [v]
        (Track.
          (get v "title")
          (map-keys (get v "meta") keyword)
          (get v "path"))))
    js->clj
    app-fs/read-yaml))

(defonce write-config
  (comp
    app-fs/write-yaml
    clj->js))

(defonce config-register
  (app-fs/register-config
    (app-fs/Config.
      :metadata
      nil
      read-config
      write-config)))

(defonce metacache
  (app-fs/get-config :metadata))

(print (get @metacache (first (keys @metacache))))

(defn get-reader [type]
  (case type
    :ogg #(.ogg md %)
    :MPEG-4 #(.id3v1 md %)
    #(.id3v2 md %)))


(defn format-meta [file meta]
  (let [meta (js->clj meta :keywordize-keys true)]
    (Track.
      (get meta :title (:name file))
      meta
      (:path file))))

(defn- get-meta-int [file stream callback]
  (let [reader (-> (:name file) get-extension get-reader)
        raw (reader stream)
        formatted (format-meta file raw)]
    (do
      (swap! metacache #(assoc % (:path file) formatted))
      (swap! fetch-manager #(update % :running dec))
      (callback formatted))))

(defn get-metadata [file callback]
  (if (contains? @metacache (:path file))
    (callback (get @metacache (:path file)))
    (swap! fetch-manager (fn [q] (update q :queue #(conj % [file callback]))))))

(defn- fetch-action [file callback]
  (fio/read-file (:path file)
    (fn [err data]
      (do
        (if err
          (print err))
        (get-meta-int file data callback)))))

(defn can-fetch? [manager]
  (let [{queue :queue running :running} manager]
    (not (or (= fetches-max running) (zero? (count queue))))))

(defn pop-fetch []
  (loop []
    (let [ manager @fetch-manager
           queue (:queue manager)
           head (peek queue)
           tail (pop queue)]
      (if (can-fetch? manager)
        (if (compare-and-set! fetch-manager manager
              (update (update manager :queue pop) :running inc))
          (apply fetch-action head)
          (recur))))))


(defn fetch-queue-watcher [key ref old-state manager]
  (let [{queue :queue running :running} manager]
    (if (can-fetch? manager)
      (pop-fetch))))

(add-watch fetch-manager :fetch-watch fetch-queue-watcher)
