(ns sonority.error
  (:require [cljs.nodejs :as nodejs]))

(nodejs/enable-util-print!)

(defn error [& args]
  (apply print args))
