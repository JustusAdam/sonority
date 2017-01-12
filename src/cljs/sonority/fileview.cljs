(ns sonority.fileview
  (:require [reagent.core :as reagent]
            [cljs.nodejs :as nodejs]
            [clojure.string :as string]
            [sonority.player :as player]
            [audio.constants :refer [audio-endings]]
            [audio.types :refer [Album Track album-identifier
                                 meta-track-to-album get-meta-val]]
            [audio.metadata :as md]
            [filesystem.path :as pathlib :refer [get-extension]]
            [filesystem.application :refer [homedir]]))

(defonce fs (nodejs/require "fs"))

(defrecord File [name path])

(nodejs/enable-util-print!)
(defonce music-folder (File.
                        "Music"
                        (pathlib/join homedir "Music")))

(defonce files (reagent/atom {}))

(defonce folder-select (reagent/atom music-folder))

(defonce search-crit (reagent/atom ""))

(defonce indexing (reagent/atom 0))

(defonce show-indexed-input (reagent/atom false))

(defonce current-expanded (reagent/atom nil))

(defn check-and-add-file [file]
  (if (contains? audio-endings (get-extension (.-path file)))
    (do
      (swap! indexing inc)
      (md/get-metadata file
        (fn [track]
          (let [meta (.-meta track)
                ai (album-identifier track)]
            (swap! files
              (fn [files]
                (update files ai
                  (fn [album]
                    (case album
                      nil (Album.
                            (get-meta-val track :album)
                            (meta-track-to-album meta)
                            [track])
                      (update-in album [:tracks] #(conj % track)))))))
            (swap! indexing dec)))))))


(defn scan-folder [folder]
  (.readdir fs (.-path folder)
    (fn [err files]
      (doall
        (for [file (map #(File. % (str (.-path folder) "/" %)) files)]
          (.stat fs (.-path file)
            (fn [err stat]
              (cond
                (.isDirectory stat) (scan-folder file)
                (.isFile stat) (check-and-add-file file)))))))))

(defn rescan-folder
  "Index a folder."
  [folder]
  (reset! files {})
  (scan-folder folder))

(defn search-bar
  "A searchbar for searching the current tracks."
  [target]
  [:div.search.row.collapse
    [:label "Search"]
    [:div.column.small-10
      [:input
        {:value @target :on-change #(reset! target (-> % .-target .-value))}]]
    [:div.column.small-2
      [:button.button.postfix
        {:on-click #(reset! search-crit "")} "x"]]])


(defn track-as-tr
  "Wrap a track and interaction elements into a table row."
  [file]
  ^{:key (.-path file)}
  [:div.track
    [:span.title
      {:on-click #(player/select-new file)}
      (:title file)]
    [:span.actions
      [:a
        {:on-click #(player/add-to-queue file)}
        "enqueue"]]])


(defn searched []
  (let [crit (string/lower-case @search-crit)
        matching
        (filter
          #(not= -1
            (.indexOf (string/lower-case (.-title %)))
            crit)
          (apply concat (map :tracks @files)))]
    [:table.search-results
      (doall
        (map track-as-tr matching))]))


(defn expand-album [ident]
  (swap! current-expanded #(if (= % ident) nil ident)))


(defn render-album-as-li [album]
  (let [title (.-title album)
        ident (album-identifier album)
        expanded @current-expanded]
    ^{:key ident}
    [:li
      [:a
        {:on-click #(expand-album ident)}
        title]
      (if (= expanded ident)
        [:div.album.songlist
          (doall
            (map
              track-as-tr
              (sort-by
                (fn [track] (get-meta-val track :tracknumber (.-title track)))
                (.-tracks album))))])]))


(defn all-albums
  []
  [:ul
    (doall
      (map
        render-album-as-li
        (sort-by
          :title
          (vals @files))))])


(defn fileview []
  [:div.row
    [:div.column.small-6
      [:h2 "I am the fileview."]
      [:a
        {:on-click #(swap! show-indexed-input not)}
        (str "Folder indexed: '" (.-path @folder-select) "'")]
      (if @show-indexed-input
        [:div
          {:class []}
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
                "Index"]]]])
      (search-bar search-crit)
      (if (empty? @search-crit)
        (all-albums)
        (searched))]
    [:div.column.small-6
      (player/std-interface)]])
