(ns clojure.files.utils
  (:require [clojure.files :as files]
            [clojure.files.file-handle :as file-handle]
            [clojure.string :as str]))

(defn search [files {:keys [folder extensions]}]
  (map (fn [path]
         [(str/replace-first path folder "") (files/internal files path)])
       (file-handle/recursively-search (files/internal files folder) extensions)))
