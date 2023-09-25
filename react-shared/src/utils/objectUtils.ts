
const getObjectProperty = (object: any, path: string, defaultValue?: string): any | undefined => {
    if (!Boolean(object)) { // null or undefined
      return defaultValue;
    }
    const parts = path.split('.');
    return parts.reduce((object, key) => object?.[key], object);
};

export { getObjectProperty };
