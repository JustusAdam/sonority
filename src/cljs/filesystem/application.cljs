(ns filesystem.application
  (:require [cljs.nodejs :as nodejs]
            [filesystem.path :as pathlib]
            [filesystem.io :as fio]
            [reagent.core :as reagent]))

(def os (nodejs/require "os"))
(def fs (nodejs/require "fs"))
(def YAML (nodejs/require "yamljs"))

(nodejs/enable-util-print!)


(def read-yaml (comp js->clj (.-parse YAML)))
(def write-yaml (comp #(.stringify YAML % 4) clj->js))
(def read-json (comp js->clj (.-parse js/JSON)))
(def write-json (comp (.-stringify js/JSON) clj->js))


(def readers
  { ".yaml" read-yaml
    ".yml" read-yaml
    ".json" read-json})
(def writers
  { ".yaml" write-yaml
    ".yml" write-yaml
    ".json" write-json})


(defn get-or-create [path]
  (do
    (if (-> path fio/exists-sync not)
      (fio/mkdir-sync path))
    path))


(def app-folder (get-or-create (pathlib/join (.homedir os) ".sonority")))
(def config-folder (get-or-create (pathlib/join app-folder "config")))


(defn- normalize-configname [name]
  (if (= "" (pathlib/get-extension name))
    (str name ".yaml")
    name))


(def get-reader (comp (partial get readers) pathlib/get-extension))
(def get-writer (comp (partial get writers) pathlib/get-extension))

(defn- write-config-file [name data]
  (let [nname (normalize-configname name)
        path (pathlib/join config-folder nname)
        writer (get-writer nname)]
    (fio/write-file path (writer data))))

(defn- get-config-file [name]
  (let [nname (normalize-configname name)
        path (pathlib/join config-folder nname)
        reader (get-reader nname)]
    (if-not (fio/exists-sync path)
      (do
        (fio/write-file-sync path ((get-writer nname) {}))
        {}))
    (let [val (reader (fio/read-file-sync path (js-obj "encoding" "utf8")))]
      (do
        (if (nil? val)
          {}
          val)))))

(def configs (reagent/atom {}))


(defn conf-watcher [name]
  (fn [key ref old-state new-state]
    (write-config-file name new-state)))

(defn- get-config-i [name converter]
  (if-not (contains? @configs name)
    (let [conf (reagent/atom (converter (get-config-file name)))]
      (do
        (add-watch conf :persistence-updater (conf-watcher name))
        (swap! configs #(assoc % name conf))
        conf))
    (get @configs name)))

(defn get-config
  ([name] (get-config-i name identity))
  ([name converter] (get-config-i name converter)))
