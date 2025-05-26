(ns gdl.application
  (:require [qrecord.core :as q])
  (:import (com.badlogic.gdx Gdx)))

(defmacro post-runnable! [& exprs]
  `(.postRunnable Gdx/app (fn [] ~@exprs)))

(q/defrecord Context [ctx/assets
                      ctx/graphics
                      ctx/stage])
