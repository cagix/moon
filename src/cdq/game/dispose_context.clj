(ns cdq.game.dispose-context
  (:require [cdq.ctx :as ctx]
            [gdl.utils :as utils]))

(defn do! []
  (utils/dispose! ctx/assets)
  (utils/dispose! ctx/batch)
  (utils/dispose! ctx/shape-drawer-texture)
  (run! utils/dispose! (vals ctx/cursors))
  (utils/dispose! ctx/default-font)
  ; TODO vis-ui dispose
  ; TODO dispose world tiled-map/level resources?
  )
