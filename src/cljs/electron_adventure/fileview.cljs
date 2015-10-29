(ns electron-adventure.fileview
  (:require [reagent.core :as reagent]
            [cljsjs.react]
            [cljs.nodejs :as nodejs]
            [clojure.string :as string]
            [electron-adventure.player :as player :refer [File]]))

(def fs (nodejs/require "fs"))

(nodejs/enable-util-print!)
(def music-folder (File. "Music" "/Users/justusadam/Music"))

(def files (reagent/atom #{}))

(def folder-select (reagent/atom music-folder))

(def audio-endings
  ["mp3", "ogg", "wav"])

(defn check-and-add-file [file]
  (if (some (partial = (last (string/split (:name file)  #"\." ))) audio-endings)
    (swap! files #(conj % file))))

(defn scan-folder [folder]
  (.readdir fs (:path folder)
    (fn [err files]
      (doall
        (for [file (map #(File. % (str (:path folder) "/" %)) files)]
          (.stat fs (:path file)
            (fn [err stat]
              (do
                (cond
                  (.isDirectory stat) (scan-folder file)
                  (.isFile stat) (check-and-add-file file))))))))))


(defn rescan-folder [folder]
  (do
    (reset! files #{})
    (scan-folder folder)))

(rescan-folder music-folder)

(defn fileview []
  [:div {:class "row"}
    [:div {:class "col-xs-6"}
      [:h2 "I am the fileview."]
      [:p (str "Folder indexed: '" (:path @folder-select) "'")]
      [:div
        [:button {:on-click #(rescan-folder @folder-select)} "Index"]]
      (doall (for [file (sort-by :name @files)]
          ^{:key (:path file)}
          [:div
            ((if (= @player/selected-piece file)
              #(assoc % :class "active")
              identity)
                {:on-click #(reset! player/selected-piece file)})
            (:name file)]))]
    [:div {:class "col-xs-6"}
      (player/player)]])
