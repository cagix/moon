(ns gdl.assets
  (:require [clojure.string :as str]
            [gdl.file :as f]))

(defn find-assets [{:keys [folder extensions]}]
  (map #(str/replace-first % (str (f/path folder) "/") "")
       (f/recursively-search folder extensions)))
