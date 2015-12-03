(ns audio.constants)


(defrecord AudioType [name aliases fileendings])


(def audio-types
  [ (AudioType. :mp3 [] ["mp3"])
    (AudioType. :ogg ["oga"] ["oga", "ogg"])
    (AudioType. :wma [] ["wma"])
    (AudioType. :MPEG-4 [] ["m4p" "mp4" "m4a"])])


(def audio-map
  (reduce #(assoc %1 (:name %2) %2) {} audio-types))

(def audio-endings
  (reduce
    (fn [map type]
      (reduce
        (fn [map ending]
          (assoc map ending type)) map (:fileendings type)))
    {}
    audio-types))

(defn type-from-ending [t]
  (let [kw  (if (keyword? t)
              t
              (keyword t))]
    (kw audio-endings)))
