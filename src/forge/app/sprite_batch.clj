(ns forge.app.sprite-batch
  (:require [anvil.disposable :as disposable]
            [anvil.graphics :refer [batch]]
            [clojure.gdx.graphics :as g]
            [clojure.utils :refer [bind-root]]))

(defn create [_]
  (bind-root batch (g/sprite-batch)))

(defn dispose [_]
  (disposable/dispose batch))
