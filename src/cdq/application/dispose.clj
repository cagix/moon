(ns cdq.application.dispose
  (:require [gdl.utils :refer [dispose!]]))

(defn do! [{:keys [ctx/assets
                   ctx/batch
                   ctx/shape-drawer-texture
                   ctx/cursors
                   ctx/default-font]}]
  (dispose! assets)
  (dispose! batch)
  (dispose! shape-drawer-texture)
  (run! dispose! (vals cursors))
  (dispose! default-font)
  ; TODO vis-ui dispose
  ; TODO dispose world tiled-map/level resources?
  )
