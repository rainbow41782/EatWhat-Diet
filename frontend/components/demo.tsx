"use client";

import { useState } from "react";
import { Component } from "./ui/sign-in-card-2";
import { Select, SelectContent, SelectItem, SelectTrigger } from "./ui/select";

const DemoOne = () => {
  return (
    <div className="flex w-full h-screen justify-center items-center">
      <Component />
    </div>
  );
};

export default function SelectDemo() {
  const [value, setValue] = useState("medium");

  return (
    <div className="flex items-center justify-center min-h-screen bg-background">
      <Select value={value} onValueChange={setValue}>
        <SelectTrigger placeholder="Select size..." />
        <SelectContent>
          <SelectItem index={0} value="small">Small</SelectItem>
          <SelectItem index={1} value="medium">Medium</SelectItem>
          <SelectItem index={2} value="large">Large</SelectItem>
          <SelectItem index={3} value="xl">Extra Large</SelectItem>
        </SelectContent>
      </Select>
    </div>
  );
}

export { DemoOne };
