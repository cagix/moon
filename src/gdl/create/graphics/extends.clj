(ns gdl.create.graphics.extends
  (:require [gdl.graphics.camera]
            [gdl.graphics.texture]
            [gdl.graphics.g2d.texture-region]
            [gdl.graphics.viewport]
            [gdx.graphics :as graphics]
            [gdx.graphics.g2d :as g2d]
            [gdx.math.vector3 :as vector3]))

(extend-type com.badlogic.gdx.utils.viewport.FitViewport
  gdl.graphics.viewport/Viewport
  (unproject [this position]
    (graphics/unproject this position)))

(extend-type com.badlogic.gdx.graphics.g2d.TextureRegion
  gdl.graphics.g2d.texture-region/TextureRegion
  (dimensions [texture-region]
    [(.getRegionWidth  texture-region)
     (.getRegionHeight texture-region)])

  (region [texture-region x y w h]
    (g2d/texture-region x y w h)))

(extend-type com.badlogic.gdx.graphics.Texture
  gdl.graphics.texture/Texture
  (region
    ([texture]
     (g2d/texture-region texture))
    ([texture x y w h]
     (com.badlogic.gdx.graphics.g2d.TextureRegion. texture
                                                   (int x)
                                                   (int y)
                                                   (int w)
                                                   (int h)))))

(extend-type com.badlogic.gdx.graphics.OrthographicCamera
  gdl.graphics.camera/Camera
  (zoom [this]
    (.zoom this))

  (position [this]
    [(.x (.position this))
     (.y (.position this))])

  (frustum [this]
    (let [frustum-points (take 4 (map vector3/clojurize (.planePoints (.frustum this))))
          left-x   (apply min (map first  frustum-points))
          right-x  (apply max (map first  frustum-points))
          bottom-y (apply min (map second frustum-points))
          top-y    (apply max (map second frustum-points))]
      [left-x right-x bottom-y top-y]))

  (set-position! [this [x y]]
    (set! (.x (.position this)) (float x))
    (set! (.y (.position this)) (float y))
    (.update this))

  (set-zoom! [this amount]
    (set! (.zoom this) amount)
    (.update this))

  (viewport-width [this]
    (.viewportWidth this))

  (viewport-height [this]
    (.viewportHeight this))

  (reset-zoom! [cam]
    (gdl.graphics.camera/set-zoom! cam 1))

  (inc-zoom! [cam by]
    (gdl.graphics.camera/set-zoom! cam (max 0.1 (+ (.zoom cam) by)))) )
