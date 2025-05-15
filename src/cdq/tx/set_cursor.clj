(ns cdq.tx.set-cursor
  (:require [cdq.ctx :as ctx]
            [cdq.utils :as utils])
  (:import (com.badlogic.gdx Gdx)))

(defn do! [cursor]
  (.setCursor Gdx/graphics (utils/safe-get (:cursors ctx/graphics) cursor)))
