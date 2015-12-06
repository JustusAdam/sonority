(ns filesystem.io
  (:require [cljs.nodejs :as nodejs]))

(def fs (nodejs/require "fs"))

(def read-file (.-readFile fs))
(def read-file-sync (.-readFileSync fs))

(def exists-sync (.-existsSync fs))
(def write-file (.-writeFile fs))
(def write-file-sync (.-writeFileSync fs))
(def mkdir (.-mkdir fs))
(def mkdir-sync (.-mkdirSync fs))
