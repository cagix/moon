(ns forge.app.sprite-batch
  (:require [clojure.gdx.graphics :as g]
            [clojure.gdx.utils.disposable :refer [dispose]]
            [forge.core :refer [batch]]
            [forge.utils :refer [bind-root]]))

(defn create [_]
  (bind-root batch (g/sprite-batch)))

(defn destroy [_]
  (dispose batch))
