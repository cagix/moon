(ns forge.app.sprite-batch
  (:require [clojure.gdx.graphics :as g]
            [clojure.gdx.utils.disposable :refer [dispose]]
            [forge.utils :refer [bind-root]]))

(declare batch)

(defn create [_]
  (bind-root batch (g/sprite-batch)))

(defn destroy [_]
  (dispose batch))
