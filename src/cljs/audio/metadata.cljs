(ns audio.metadata
  (:require [cljs.nodejs :as nodejs]
            [audio.constants :as cs]
            [audio.types :as atypes :refer [Track]]
            [filesystem.path :refer [get-extension]]
            [reagent.core :as reagent]
            [filesystem.application :as app-fs]
            [filesystem.io :as fio]
            [sonority.util :refer [map-vals]]))


(def md (nodejs/require "audio-metadata"))
(def fs (nodejs/require "fs"))

(nodejs/enable-util-print!)

(def metacache
  (app-fs/get-config "metadata"
    #(map-vals
      (fn [v]
        (Track.
          (get v "title")
          (get v "meta")
          (get v "path"))) %)))

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
      (callback formatted))))

(defn get-metadata [file callback]
  (if (contains? @metacache (:path file))
    (do
      (print "skipping " file)
      (callback (get @metacache (:path file))))
    (do
      (fio/read-file (:path file)
        (fn [err data]
          (if err
            (print err))
          (get-meta-int file data callback))))))
