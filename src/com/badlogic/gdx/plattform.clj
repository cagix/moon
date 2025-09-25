(ns com.badlogic.gdx.plattform
  (:require [gdl.plattform :as plattform]
            [com.badlogic.gdx.scenes.scene2d.stage :as stage]))

(def impl
  (reify plattform/Plattform
    (stage [_ viewport batch]
      (stage/create viewport batch))))
