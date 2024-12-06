(ns forge.app.sprite-batch
  (:require [clojure.gdx.graphics :as g]
            [forge.core :refer [bind-root
                                dispose
                                batch]]))

(defn create [_]
  (bind-root batch (g/sprite-batch)))

(defn destroy [_]
  (dispose batch))
