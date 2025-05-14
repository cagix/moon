(ns cdq.game.dispose-context
  (:require [cdq.ctx :as ctx])
  (:import (com.badlogic.gdx.utils Disposable)))

(defn do! []
  (Disposable/.dispose ctx/assets)
  (Disposable/.dispose ctx/graphics)
  ; TODO vis-ui dispose
  ; TODO dispose world tiled-map/level resources?
  )
