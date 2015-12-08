(ns sonority.core
  (:require [reagent.core :as reagent]
            [cljsjs.react]
            [sonority.fileview :as fv]
            [sonority.alert :as alert]))

(defn main-page
  []
  [:div.container.pageheight
   (fv/fileview)
   (alert/alert-display)])

(defn mount-root
  []
  (reagent/render [main-page] (.getElementById js/document "app")))

(defn init!
  []
  (do
    (alert/show-alert (alert/return-value-alert "Testalert" ["No" "Yes" "Maybe" "Never"] alert/hide-alert))
    (mount-root)))
