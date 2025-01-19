(ns cdq.gdx.graphics)

(defn clear-screen [context]
  (com.badlogic.gdx.utils.ScreenUtils/clear com.badlogic.gdx.graphics.Color/BLACK)
  context)

(defn sprite-batch [_context]
  (com.badlogic.gdx.graphics.g2d.SpriteBatch.))

(defn def-color [name-str color]
  (com.badlogic.gdx.graphics.Colors/put name-str color))
