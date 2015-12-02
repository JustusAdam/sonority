(ns audio.types)

(defrecord Album [title meta tracks])

(defrecord Track [title meta path])

(defn get-meta-val
  ([obj key val] (get (:meta obj) key val))
  ([obj key]
    (let [v (get (:meta obj) key)]
      (case v
        nil (case key
              :artist "Unknown Artist"
              :album "Unknown Album"
              :tracknumber 0
              nil)
        v))))

(defn album-identifier [album]
  (str (:title album) "-" (get-meta-val album :artist) "-" (get-meta-val album :year)))

(defn meta-track-to-album [meta]
  (let [albumtitle (:album meta)]
    (assoc
      (dissoc meta :tracknumber :title :track)
      :title albumtitle)))
