(ns electron-adventure.fileview
  (:require [reagent.core :as reagent]
            [cljsjs.react]
            [cljs.nodejs :as nodejs]))

(def fs (nodejs/require "fs"))

;(nodejs/enable-util-print!)

(def files (reagent/atom ["."]))

(def selected (reagent/atom nil))

(.readdir fs "." #(reset! files %2))

(defn fileview []
  [:div
    [:p (str "I am the fileview. Currently selected: " @selected)]
    (for [file @files] ^{:key file} [:div {:on-click #(reset! selected file)} file])])
