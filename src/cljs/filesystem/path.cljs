(ns filesystem.path
  (:require [clojure.string :as string]))


(defn get-extension [path] (last (string/split path  #"\.")))
