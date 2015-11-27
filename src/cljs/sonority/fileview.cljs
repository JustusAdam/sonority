(ns sonority.fileview
  (:require [reagent.core :as reagent]
            [cljsjs.react]
            [cljs.nodejs :as nodejs]
            [clojure.string :as string]
            [sonority.player :as player :refer [File]]))

(def fs (nodejs/require "fs"))

(nodejs/enable-util-print!)
(def music-folder (File. "Music" "/Users/justusadam/Music"))

(def files (reagent/atom #{}))

(def folder-select (reagent/atom music-folder))

(def search-crit (reagent/atom ""))

(def audio-endings
  ["mp3" "ogg" "wav" "m4a"])

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
              (cond
                (.isDirectory stat) (scan-folder file)
                (.isFile stat) (check-and-add-file file)))))))))

(defn rescan-folder [folder]
  (do
    (reset! files #{})
    (scan-folder folder)))

(rescan-folder @folder-select)

(defn search-bar [target]
  [:div.search
    [:label "Search"]
    [:input {:value @target :on-change #(reset! target (-> % .-target .-value))}]
    [:button {:on-click #(reset! search-crit "")} "x"]])

(defn fileview []
  [:div.row
    [:div.column.small-6
      [:h2 "I am the fileview."]
      [:p (str "Folder indexed: '" (:path @folder-select) "'")]
      [:div
        [:div
          [:label {:for "pick-folder"} "Select indexed folder"]
          [:input#pick-folder {:type "text"
                               :on-change #(reset! folder-select (let [val (-> % .-target .-value)]
                                                                      (File. val val)))}]]
        [:button {:on-click #(rescan-folder @folder-select)} "Index"]]
      (search-bar search-crit)
      [:table
        (let [crit (string/lower-case @search-crit)
              selected @player/selected-piece]
          (doall (for [file (filter #(not= -1 (.indexOf (string/lower-case (:name %)) crit)) (sort-by :name @files))]
                  ^{:key (:path file)}
                  [:tr
                    ((if (= selected file)
                      #(assoc % :class "active")
                      identity)
                     {:on-click #(player/select-new file)})
                    [:td (:name file)]
                    [:td [:a {:on-click #(player/add-to-queue file)} "enqueue"]]])))]]
    [:div.col-xs-6
      (player/std-interface)]])
