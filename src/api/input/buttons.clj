(ns api.input.buttons
  (:require [utils.reflect :refer [bind-roots]]))

(declare back
         forward
         left
         middle
         right)

(bind-roots "com.badlogic.gdx.Input$Buttons" 'int "api.input.buttons")
