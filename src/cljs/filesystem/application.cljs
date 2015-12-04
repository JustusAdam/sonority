(ns filesystem.application
  (:require [cljs.nodejs :as nodejs]
            [filesystem.path :as pathlib]))

(def os (nodejs/require "os"))
(def fs (nodejs/require "fs"))


(def app-folder
  (let [val (pathlib/join (.homedir os) ".sonority")]
    (do
      (if (-> val pathlib/exists not)
        (.mkdir fs val)))
    val))
