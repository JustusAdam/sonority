(ns audio.constants)


(defrecord AudioType [name aliases fileendings])


(def audio-types
  [ (AudioType. :mp3 [] [".mp3"])
    (AudioType. :ogg ["oga"] [".oga", ".ogg"])
    (AudioType. :wma [] [".wma"])])


(def audio-map
  (reduce #(assoc %1 (:name %2) %2) {} audio-types))

(def audio-endings
  (reduce
    (fn [map type]
      (for [ending (:fileendings type)]
        (assoc map (keyword ending) type)))
    {}
    audio-types))

(defn type-from-ending [t]
  (let [kw  (if (keyword? t)
              t
              (keyword t))]
    (kw audio-endings)))
