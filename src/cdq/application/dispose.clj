(ns cdq.application.dispose
  (:require [cdq.ctx :as ctx]
            [gdl.utils :refer [dispose!]]))

(defn do! []
  (dispose! ctx/assets)
  (dispose! ctx/batch)
  (dispose! ctx/shape-drawer-texture)
  (run! dispose! (vals ctx/cursors))
  (dispose! ctx/default-font)
  ; TODO vis-ui dispose
  ; TODO dispose world tiled-map/level resources?
  )
