(ns filesystem.application
  (:require [cljs.nodejs :as nodejs]
            [filesystem.path :as pathlib]
            [filesystem.io :as fio]
            [sonority.error :refer [error]]))

(def os (nodejs/require "os"))
(def fs (nodejs/require "fs"))
(def YAML (nodejs/require "yamljs"))

(nodejs/enable-util-print!)


(defrecord Config [name path reader writer])


(def read-yaml (.-parse YAML))
(def write-yaml #(.stringify YAML % 4))
(def read-json (.-parse js/JSON))
(def write-json (.-stringify js/JSON))

(defn get-or-create [path]
  (do
    (if (-> path fio/exists-sync not)
      (fio/mkdir-sync path))
    path))


(def homedir (.homedir os))
(def app-folder (get-or-create (pathlib/join homedir ".sonority")))
(def config-folder (get-or-create (pathlib/join app-folder "config")))


(defn- normalize-configname [name]
  (if (= "" (pathlib/get-extension name))
    (str name ".yaml")
    name))

(defn make-default-config-path [name]
  (let [name (normalize-configname (str name))]
    (pathlib/join config-folder name)))

(defn normalize-config [config]
  (if (nil? (:path config))
    (assoc config :path (make-default-config-path (:name config)))
    config))

(defn- write-config-file [config data]
  (let [{writer :writer path :path} config]
    (fio/write-file path (writer data))))

(defn- get-config-file [config]
  (let [{path :path name :name reader :reader writer :writer} config]
    (if-not (fio/exists-sync path)
        (do
          (fio/write-file-sync path (writer {}))
          {})
      (let [val (reader (fio/read-file-sync path (js-obj "encoding" "utf8")))]
        (do
          (if (nil? val)
            {}
            val))))))

(def config-data (atom {}))
(def configs (atom {}))

(defn register-config [config]
  (if (contains? (:name config) @configs)
    (error "Reregistering config is not allowed")
    (let [config (normalize-config config)]
      (swap! configs #(assoc % (:name config) config)))))


(defn conf-watcher [name]
  (fn [key ref old-state new-state]
    (write-config-file (get @configs name) new-state)))

(defn get-config [name]
  (if-not (contains? @configs name)
    (error "Attemting to read unregistered config " name)
    (if-not (contains? @config-data name)
      (let [meta (get @configs name)
            {reader :reader} meta
            conf (atom (get-config-file meta))]
        (do
          (add-watch conf :persistence-updater (conf-watcher name))
          (swap! config-data #(assoc % name conf))
          conf))
      (get @config-data name))))
