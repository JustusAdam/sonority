(ns audio.metadata
  (:require [cljs.nodejs :as nodejs]
            [audio.constants :as cs]))


(def md (nodejs/require "audio-metadata"))
(def fs (nodejs/require "fs"))


(nodejs/enable-util-print!)


(defn get-reader [type]
  (case type
    :ogg #(.ogg md %)
    #(.id3v2 md %)))


(defn format-meta [map]
  (js->clj {:keywordize true} map))


(defn- get-meta-int [file stream callback]
  (-> stream (-> type :name get-reader) format-meta callback))


(defn get-metadata
  ([file callback] (.readFile fs file)
    (fn [err data]
      (if err
        (print err)
        (get-meta-int file data callback))))
  ([file stream callback] (get-meta-int file stream callback)))
