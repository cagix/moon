(ns clojure.gdx
  (:import (com.badlogic.gdx.utils SharedLibraryLoader
                                   Os)))

(defn operating-system []
  (let [os SharedLibraryLoader/os]
    (cond (= os Os/MacOsX) :os/mac)))
