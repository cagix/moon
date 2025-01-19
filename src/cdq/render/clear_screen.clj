(ns cdq.render.clear-screen)

(defn render [context]
  (com.badlogic.gdx.utils.ScreenUtils/clear com.badlogic.gdx.graphics.Color/BLACK)
  context)
