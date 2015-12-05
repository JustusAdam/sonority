(ns filesystem.path
  (:require [clojure.string :as string]
            [cljs.nodejs :as nodejs]))


(def pathlib (nodejs/require "path"))


(defn get-extension [path] (.extname pathlib path))

(defn join [& parts] (apply (.-join pathlib) parts))

(defn exists [path] (.exists pathlib path))
