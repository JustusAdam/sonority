(ns audio.metadata
  (:require [cljs.nodejs :as nodejs]
            [audio.constants :as cs]
            [filesystem.path :refer [get-extension]]
            [reagent.core :as reagent]))


(def md (nodejs/require "audio-metadata"))
(def fs (nodejs/require "fs"))


(nodejs/enable-util-print!)


(def should-print (reagent/atom 3))


(defn get-reader [type]
  (case type
    :ogg #(.ogg md %)
    #(.id3v2 md %)))


(defn format-meta [meta] (js->clj meta :keywordize-keys true))


(defn- get-meta-int [file stream callback]
  (let [reader (-> (:name file) get-extension get-reader)
        raw (reader stream)
        formatted (format-meta raw)]
    (callback formatted)))


(defn get-metadata
  ([file callback] (.readFile fs (:path file)
                    (fn [err data]
                      (if err
                        (print err)
                        (get-meta-int file data callback)))))
  ([file stream callback] (get-meta-int file stream callback)))
