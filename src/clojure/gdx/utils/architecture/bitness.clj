(ns clojure.gdx.utils.architecture.bitness
  (:import (com.badlogic.gdx.utils Architecture$Bitness)))

(def mapping {Architecture$Bitness/_128 :architecture.bitness/_128
              Architecture$Bitness/_32  :architecture.bitness/_32
              Architecture$Bitness/_64  :architecture.bitness/_64})
