(ns electron-adventure.core
  (:require [reagent.core :as reagent]
            [cljsjs.react]
            [electron-adventure.fileview :as fv]))

(defn main-page
  []
  [:div {:class "container-fluid"}
      (fv/fileview)])

(defn mount-root
  []
  (reagent/render [main-page] (.getElementById js/document "app")))

(defn init!
  []
  (mount-root))
