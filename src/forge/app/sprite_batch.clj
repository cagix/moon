(ns forge.app.sprite-batch
  (:require [anvil.graphics :refer [batch]]
            [clojure.gdx.graphics :as g]
            [clojure.gdx.utils.disposable :refer [dispose]]
            [clojure.utils :refer [bind-root]]))

(defn create [_]
  (bind-root batch (g/sprite-batch)))

(defn destroy [_]
  (dispose batch))
