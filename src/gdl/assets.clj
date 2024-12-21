(ns gdl.assets
  (:require [clojure.gdx.audio.sound :as sound]
            [clojure.gdx.assets :as assets]
            [clojure.gdx.files :as files]
            [clojure.gdx.files.file-handle :as fh]
            [clojure.string :as str]))

(defn- load-all [manager assets]
  (doseq [[file asset-type] assets]
    (assets/load manager file asset-type))
  (assets/finish-loading manager))

(defn- recursively-search [folder extensions]
  (loop [[file & remaining] (fh/list (files/internal folder))
         result []]
    (cond (nil? file)
          result

          (fh/directory? file)
          (recur (concat remaining (fh/list file)) result)

          (extensions (fh/extension file))
          (recur remaining (conj result (fh/path file)))

          :else
          (recur remaining result))))

(def folder "resources/")

(def asset-type-exts {:sound   #{"wav"}
                      :texture #{"png" "bmp"}})

(defn setup []
  (def manager (doto (assets/manager)
                 (load-all (for [[asset-type exts] asset-type-exts
                                 file (map #(str/replace-first % folder "")
                                           (recursively-search folder exts))]
                             [file asset-type])))))

(defn cleanup []
  (assets/dispose manager))

(def sound-asset-format "sounds/%s.wav")

(defn play-sound [sound-name]
  (->> sound-name
       (format sound-asset-format)
       manager
       sound/play))

(defn all-of-type
  "Returns all asset paths with the specific asset-type."
  [asset-type]
  (filter #(= (assets/type manager %) asset-type)
          (assets/names manager)))
