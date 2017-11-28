(ns org-z-me.subs
  (:require [re-frame.core :refer [reg-sub]]))

(reg-sub
 :get-greeting
 (fn [db _]
   (:greeting db)))

(reg-sub
 :get-val
 (fn [db [_ k]]
   (k db)))
