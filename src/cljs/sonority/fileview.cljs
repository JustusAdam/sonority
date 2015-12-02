(ns sonority.fileview
  (:require [reagent.core :as reagent]
            [cljsjs.react]
            [cljs.nodejs :as nodejs]
            [clojure.string :as string]
            [sonority.player :as player]
            [audio.constants :refer [audio-endings]]
            [audio.types :refer [Album Track album-identifier meta-track-to-album get-meta-val]]
            [audio.metadata :as md]
            [filesystem.path :refer [get-extension]]))

(def fs (nodejs/require "fs"))

(defrecord File [name path])

(nodejs/enable-util-print!)
(def music-folder (File. "Music" "/Users/justusadam/Music"))

(def files (reagent/atom {}))

(def folder-select (reagent/atom music-folder))

(def search-crit (reagent/atom ""))

(defn check-and-add-file [file]
  (if (contains? audio-endings (get-extension (:path file)))
    (md/get-metadata file
      (fn [meta]
        (let [track (Track. (get meta :title (:name file)) meta (:path file))
              ai (album-identifier track)]
          (swap! files
            (fn [files]
              (update files ai
                (fn [album]
                  (case album
                    nil (Album. (meta-track-to-album meta) (get-meta-val track :album) #{track})
                    (update album :tracks #(conj % track))))))))))))


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
    (reset! files {})
    (scan-folder folder)))

(rescan-folder @folder-select)

(defn search-bar [target]
  [:div.search
    [:label "Search"]
    [:input {:value @target :on-change #(reset! target (-> % .-target .-value))}]
    [:button {:on-click #(reset! search-crit "")} "x"]])


(defn track-as-tr
  [file]
  [:tr {:on-click #(player/select-new file)}
    [:td (:title file)]
    [:td [:a {:on-click #(player/add-to-queue file)} "enqueue"]]])


(defn searched []
  (let [crit (string/lower-case @search-crit)
        matching
          (filter
            #(not= -1 (.indexOf (string/lower-case (:title %)) crit))
            (apply concat (map :tracks @files)))]
    [:table.search-results
      (doall
        (map track-as-tr matching))]))


(defn all-albums
  []
  [:ul.accordion
    (doall
      (for [album (sort-by :title (vals @files))]
        ^{:key (:path album)}
        [:li.accordion-navigation
          [:a {:href (-> album :title (partial str "#"))}
            [:div {:id (:title album)}
              [:table
                (doall
                  (map track-as-tr (sort-by #(get-meta-val % :tracknumber) (:tracks album))))]]]]))])


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
      (if (empty? @search-crit)
        (all-albums)
        (searched))]
    [:div.col-xs-6
      (player/std-interface)]])
