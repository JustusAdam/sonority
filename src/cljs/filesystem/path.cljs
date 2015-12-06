(ns filesystem.path
  (:require [clojure.string :as string]
            [cljs.nodejs :as nodejs]))


(def pathlib (nodejs/require "path"))


(defonce basename (.-basename pathlib))
(defonce delimiter (.-delimiter pathlib))
(defonce dirname (.-dirname pathlib))
(defonce get-extension (.-extname pathlib))
(defonce format (.-format pathlib))
(defonce is-absolute (.-isAbsolute pathlib))
(defonce normalize (.-normalize pathlib))
(defonce parse (.-parse pathlib))
(defonce posix (.-posix pathlib))
(defonce relative (.-relative pathlib))
(defonce resolve (.-resolve pathlib))
(defonce sep (.-sep pathlib))
(defonce win32 (.-win32 pathlib))
(defonce join (.-join pathlib))
