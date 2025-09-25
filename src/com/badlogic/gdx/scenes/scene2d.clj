(ns com.badlogic.gdx.scenes.scene2d)

(defmulti build
  "Builds the `com.badlogic.gdx.scenes.scene2d.actor` via the supplied options map.

  Dispatches on `:actor/type`."
  :actor/type)
