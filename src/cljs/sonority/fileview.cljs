(ns sonority.fileview
  (:require [reagent.core :as reagent]
            [cljsjs.react]
            [cljs.nodejs :as nodejs]
            [clojure.string :as string]
            [sonority.player :as player]
            [audio.constants :refer [audio-endings]]
            [audio.types :refer [ Album Track album-identifier
                                  meta-track-to-album get-meta-val]]
            [audio.metadata :as md]
            [filesystem.path :refer [get-extension]]))

(defonce fs (nodejs/require "fs"))

(defrecord File [name path])

(defrecord WrappedAlbum [album expanded])

(nodejs/enable-util-print!)
(defonce music-folder (File.
                        "Music"
                        "/Users/justusadam/Music/iTunes/iTunes Media/Music/Nigel Stanford"))

(defonce files (reagent/atom {}))

(defonce folder-select (reagent/atom music-folder))

(defonce search-crit (reagent/atom ""))

(defonce indexing (reagent/atom 0))

(defonce show-indexed-input (reagent/atom false))

(defn check-and-add-file [file]
  (if (contains? audio-endings (get-extension (:path file)))
    (do
      (swap! indexing inc)
      (md/get-metadata file
        (fn [meta]
          (let [track (Track.
                        (get meta :title (:name file))
                        meta
                        (:path file))
                ai (album-identifier track)]
            (do
              (swap! files
                (fn [files]
                  (update files ai
                    (fn [album]
                      (case album
                        nil (WrappedAlbum.
                              (Album.
                                (get-meta-val track :album)
                                (meta-track-to-album meta)
                                [track])
                              false)
                        (update-in album [:album :tracks] #(conj % track)))))))
              (swap! indexing dec))))))))


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

(defn search-bar [target]
  [:div.search.row.collapse
    [:label "Search"]
    [:div.column.small-10
      [:input
        {:value @target :on-change #(reset! target (-> % .-target .-value))}]]
    [:div.column.small-2
      [:button.button.postfix
        {:on-click #(reset! search-crit "")} "x"]]])


(defn track-as-tr
  [file]
  ^{:key (:path file)}
  [:tr
    [:td
      {:on-click #(player/select-new file)}
      (:title file)]
    [:td
      [:a
        {:on-click #(player/add-to-queue file)}
        "enqueue"]]])


(defn searched []
  (let [crit (string/lower-case @search-crit)
        matching
          (filter
            #(not= -1
              (.indexOf (string/lower-case (:title %)))
              crit)
            (apply concat (map :tracks @files)))]
    [:table.search-results
      (doall
        (map track-as-tr matching))]))


(defn expand-album [album]
  (let [ident (if (number? album)
                album
                (album-identifier album))]
    (swap! files
      (fn [files]
        (update-in files [ident :expanded] not)))))


(defn all-albums
  []
  [:ul
    (doall
      (for [wrapped (sort-by
                      #(get-meta-val % :title)
                      (vals @files))]
        (let [album (:album wrapped)
              title (:title album)
              ident (album-identifier album)]
          ^{:key ident}
          [:li
            [:a
              {:on-click #(expand-album ident)}
              title]
            [:div
              {:class [ (if (:expanded wrapped)
                          "expanded"
                          "collapsed")]}
              [:table
                (doall
                  (map track-as-tr (sort-by #(get-meta-val % :tracknumber) (:tracks album))))]]])))])


(defn fileview []
  [:div.row
    [:div.column.small-6
      [:h2 "I am the fileview."]
      [:a
        {:on-click #(swap! show-indexed-input not)}
        (str "Folder indexed: '" (:path @folder-select) "'")]
      [:div
        {:class [(if @show-indexed-input "expanded" "collapsed")]}
        [:p
          (str "Status: "
                  (if (zero? @indexing)
                    "Finished"
                    "Indexing ..."))]
        [:div.row.collapse
          [:label
            {:for "pick-folder"}
            "Select indexed folder"]
          [:div.column.small-10
            [:input#pick-folder
              { :type "text"
                :on-change #(reset! folder-select
                              (let [val (-> % .-target .-value)]
                                (File. val val)))}]]
          [:div.column.small-2
            [:button.button.postfix
              {:on-click #(rescan-folder @folder-select)}
              "Index"]]]]
      (search-bar search-crit)
      (if (empty? @search-crit)
        (all-albums)
        (searched))]
    [:div.col-xs-6
      (player/std-interface)]])
