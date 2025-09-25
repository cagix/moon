(ns com.badlogic.gdx.plattform
  (:require gdl.plattform
            [com.badlogic.gdx.scenes.scene2d.stage :as stage]))

(def impl
  (reify gdl.plattform/Plattform
    (stage [_ viewport batch]
      (stage/create viewport batch))))
