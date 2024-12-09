(ns clojure.gdx.files
  (:require [clojure.gdx.files.file-handle :as f])
  (:import (com.badlogic.gdx Gdx)
           (com.badlogic.gdx.files FileHandle)))

(defn internal ^FileHandle [path]
  (.internal Gdx/files path))

(defn recursively-search [folder extensions]
  (loop [[file & remaining] (f/list (internal folder))
         result []]
    (cond (nil? file)
          result

          (f/directory? file)
          (recur (concat remaining (f/list file)) result)

          (extensions (f/extension file))
          (recur remaining (conj result (f/path file)))

          :else
          (recur remaining result))))
