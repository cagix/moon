(ns gdl.scene2d
  (:import (com.badlogic.gdx.scenes.scene2d StageWithCtx)))

(defmulti build
  "Builds the `gdl.scene2d.actor` via the supplied options map.

  Dispatches on `:actor/type`."
  :actor/type)

(defn stage [viewport batch]
  (StageWithCtx. viewport batch))
