(ns cdq.game.dispose-context
  (:require [cdq.ctx :as ctx])
  (:import (com.badlogic.gdx.utils Disposable)))

(defn do! []
  (Disposable/.dispose ctx/assets)
  (Disposable/.dispose ctx/batch)
  (Disposable/.dispose ctx/shape-drawer-texture)
  (run! Disposable/.dispose (vals ctx/cursors))
  (Disposable/.dispose ctx/default-font)
  ; TODO vis-ui dispose
  ; TODO dispose world tiled-map/level resources?
  )
