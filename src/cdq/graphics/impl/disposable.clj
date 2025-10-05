(ns cdq.graphics.impl.disposable
  (:require [clojure.disposable :refer [dispose!]]))

(defn dispose!
  [{:keys [graphics/batch
           graphics/cursors
           graphics/default-font
           graphics/shape-drawer-texture
           graphics/textures]}]
  (dispose! batch)
  (run! dispose! (vals cursors))
  (dispose! default-font)
  (dispose! shape-drawer-texture)
  (run! dispose! (vals textures)))
