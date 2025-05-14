(ns cdq.game.load-assets
  (:require [cdq.ctx :as ctx]
            [cdq.impl.assets :as assets]
            [cdq.utils :as utils])
  (:import (com.badlogic.gdx.audio Sound)
           (com.badlogic.gdx.graphics Texture)))

(defn do! []
  (utils/bind-root #'ctx/assets (assets/create
                                 {:folder "resources/"
                                  :asset-type-extensions {Sound   #{"wav"}
                                                          Texture #{"png" "bmp"}}})) )
